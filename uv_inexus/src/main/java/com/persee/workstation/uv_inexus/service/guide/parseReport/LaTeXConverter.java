/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.parseReport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.mariuszgromada.math.mxparser.Expression;
import org.springframework.stereotype.Service;

import com.persee.workstation.uv_inexus.model.R;

@Service("LaTeXConverter")
public class LaTeXConverter {

    /**
     * 【新增核心方法】
     * 校验一个公式列表，确保每个公式使用的参数都包含在给定的总参数列表中。
     * 总参数列表可以有多余参数，但公式不能使用列表之外的参数。
     *
     * @param totalParamList 允许的总参数列表
     * @param latexFormulaList 待校验的LaTeX公式列表
     * @return "true" 如果所有公式都有效；否则返回错误信息，指明具体哪个公式的哪个参数出错
     */
    public R<String> verifyFormulasAgainstParamList(List<String> totalParamList, List<String> latexFormulaList) {
        // 0. 首先校验总参数列表本身的格式是否合法
        String paramFormatError = validateParameterFormat(totalParamList);
        if (paramFormatError != null) {
            return R.fail( "总参数列表格式错误: " + paramFormatError);
        }

        // 为了提高查找效率，将总参数列表转换为Set
        Set<String> totalParamSet = totalParamList.stream()
                .map(String::trim)
                .collect(Collectors.toSet());

        // 1. 循环校验每一个公式
        for (int i = 0; i < latexFormulaList.size(); i++) {
            String latexFormula = latexFormulaList.get(i);
            String currentFormulaIdentifier = "公式 " + (i + 1) + " (\"" + latexFormula + "\")";

            try {
                // 1.1. 将LaTeX转换为普通表达式
                String expression = convertLaTeXToExpression(latexFormula);
                if (expression == null) {
                    return R.fail( currentFormulaIdentifier + " 转换失败: 不支持的LaTeX语法");
                }

                // 1.2. 验证表达式语法是否有效
                if (!isValidExpression(expression)) {
                    return R.fail(currentFormulaIdentifier + " 语法无效: " + expression);
                }

                // 1.3. 提取等号右边的表达式
                String rightHandSide = extractRightHandSide(expression);
                if (rightHandSide == null) {
                    return R.fail(currentFormulaIdentifier + " 格式错误: 缺少等号");
                }

                // 1.4. 提取当前公式中使用的所有参数
                Set<String> formulaParams = extractParameters(rightHandSide);

                // 1.5. 【核心校验逻辑】检查当前公式的参数是否是总参数列表的子集
                List<String> invalidParams = formulaParams.stream()
                        .filter(param -> !totalParamSet.contains(param))
                        .collect(Collectors.toList());

                // 1.6. 如果发现无效参数，立即构建并返回错误信息
                if (!invalidParams.isEmpty()) {
                    return R.fail(currentFormulaIdentifier + " 包含未定义的参数: " + String.join(", ", invalidParams));
                }

            } catch (Exception e) {
                // 捕获处理单个公式时可能发生的任何意外错误
                return R.fail("00520001", currentFormulaIdentifier, e.getMessage());
            }
        }

        // 2. 如果循环正常结束，说明所有公式都通过了校验
        return R.ok();
    }

    /**
     * 【旧方法，已废弃或可保留作为单公式校验】
     * 验证单个LaTeX公式并检查参数是否与列表完全匹配（不多不少）。
     * 此方法已不再是核心，但可以保留以备不时之需。
     */
    @Deprecated
    public String verifyAndConvert(List<String> paramList, String latexFormula) {

        List<String> formulaList = Arrays.asList(latexFormula);
        R<String> result = verifyFormulasAgainstParamList(paramList, formulaList);
        if (result.getSuccess()) {
            // 新方法只检查“不多”，我们还需要检查“不少”
            Set<String> formulaParams = extractParameters(extractRightHandSide(convertLaTeXToExpression(latexFormula)));
            Set<String> paramSet = paramList.stream().map(String::trim).collect(Collectors.toSet());
            List<String> missingParams = paramSet.stream().filter(param -> !formulaParams.contains(param)).collect(Collectors.toList());
            if (!missingParams.isEmpty()) {
                return "参数不匹配: 公式缺少参数: " + String.join(", ", missingParams);
            }
        }
        return result.getMessage();
    }


    /**
     * 校验参数列表格式，+数字
     */
    private String validateParameterFormat(List<String> paramList) {
        if (paramList == null) {
            return "参数列表不能为null";
        }
        Pattern pattern = Pattern.compile("^[a-zA-Z][a-zA-Z0-9]*$");
        List<String> invalidParams = new ArrayList<>();
        for (String param : paramList) {
            if (!pattern.matcher(param).matches()) {
                invalidParams.add(param);
            }
        }

        if (!invalidParams.isEmpty()) {
            return "参数格式错误，必须是字母开头，且只能包含字母和数字: " + String.join(", ", invalidParams);
        }
        return null;
    }

    /**
     * 提取等号右边的表达式
     */
    private String extractRightHandSide(String expression) {
        String[] parts = expression.split("=", 2);
        if (parts.length < 2) {
            return null;
        }
        return parts[1].trim();
    }

    /**
     * 将LaTeX公式转换为普通数学表达式
     */
    public String convertLaTeXToExpression(String latex) {
        if (latex == null || latex.isEmpty()) {
            return null;
        }
        String expr = latex;
        // 处理分数: \frac{分子}{分母} → (分子)/(分母)
        expr = expr.replaceAll("\\\\frac\\{(.*?)\\}\\{(.*?)\\}", "($1)/($2)");
        // 处理乘法符号: \cdot → *
        expr = expr.replaceAll("\\\\cdot", "*");
        expr = expr.replaceAll("\\\\times", "*");
        // 处理左括号: \left( → (
        expr = expr.replaceAll("\\\\left\\(", "(");
        // 处理右括号: \right) → )
        expr = expr.replaceAll("\\\\right\\)", ")");
        // 处理平方根: \sqrt{x} → sqrt(x)
        expr = expr.replaceAll("\\\\sqrt\\{(.*?)\\}", "sqrt($1)");
        // 处理上标: x^{n} → x^n
        expr = expr.replaceAll("(\\w)\\^\\{(.*?)\\}", "$1^$2");
        // 处理下标: x_{n} → x_n
        expr = expr.replaceAll("(\\w)\\_\\{(.*?)\\}", "$1_$2");
        // 处理希腊字母
        expr = expr.replaceAll("\\\\α", "alpha");
        expr = expr.replaceAll("\\\\β", "beta");
        expr = expr.replaceAll("\\\\θ", "theta");
        expr = expr.replaceAll("π", "pi");
        expr = expr.replaceAll("\\\\rho", "rho");
        expr = expr.replaceAll("\\\\omega", "omega");
        // 处理数学函数：\log x → log(x), \ln x → ln(x)
        // 优先处理 \log_{n}（带底数的对数，如 \log_{10}x → log10(x)）
        expr = expr.replaceAll("\\\\log\\_\\{(\\d+)\\}(\\w)", "log$1($2)");
        expr = expr.replaceAll("\\\\log\\_\\{(\\d+)\\}", "log$1");
        // 处理 \log_n（单字符底数，如 \log_5x → log5(x)）
        expr = expr.replaceAll("\\\\log\\_(\\w)(\\w)", "log$1($2)");
        expr = expr.replaceAll("\\\\log\\_(\\w)", "log$1");
        // 匹配 \log{内容} 或 \log(内容) 或 \log 后面直接跟内容
        expr = expr.replaceAll("\\\\log\\{(.*?)\\}", "log($1)");
        expr = expr.replaceAll("\\\\log\\((.*?)\\)", "log($1)");
        expr = expr.replaceAll("\\\\log(\\d+)", "log($1)");
        expr = expr.replaceAll("\\\\log(\\w)", "log($1)");
        // 处理 ln
        expr = expr.replaceAll("\\\\ln\\{(.*?)\\}", "ln($1)");
        expr = expr.replaceAll("\\\\ln\\((.*?)\\)", "ln($1)");
        expr = expr.replaceAll("\\\\ln(\\d+)", "ln($1)");
        expr = expr.replaceAll("\\\\ln(\\w)", "ln($1)");
        // 处理积分符号: \int_{a}^{b} → int_a^b, \int → int
        expr = expr.replaceAll("\\\\int\\_\\{(.*?)\\}\\^\\{(.*?)\\}", "int_$1^$2");
        expr = expr.replaceAll("\\\\int", "int");
        // 处理微分符号: \mathrm{d} → d
        expr = expr.replaceAll("\\\\mathrm\\{d\\}", "d");
        // 清理剩余的反斜杠
        expr = expr.replaceAll("\\\\", "");
        expr = expr.replace("E", "1");
        return expr;
    }

    /**
     * 验证表达式是否可计算
     */
    private boolean isValidExpression(String expression) {
        String tempExpr = replaceVariablesWithDefault(expression);
        Expression expr = new Expression(tempExpr);
        return expr.checkSyntax();
    }

    /**
     * 提取表达式中的变量参数
     */
    private Set<String> extractParameters(String expression) {
        // 正则表达式匹配以字母或下划线开头，后跟字母、数字或下划线的变量名
        Pattern pattern = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");
        Matcher matcher = pattern.matcher(expression);
        Set<String> variables = new HashSet<>();
        while (matcher.find()) {
            String param = matcher.group();
            if (!isReservedKeyword(param)) {
                variables.add(param);
            }
        }
        return variables;
    }

    /**
     * 将变量替换为默认值，用于语法检查
     */
    private String replaceVariablesWithDefault(String expression) {
        Set<String> variables = extractParameters(expression);
        String tempExpr = expression;
        for (String var : variables) {
            tempExpr = tempExpr.replace(var, "1");
        }
        return tempExpr;
    }

    /**
     * 检查是否为系统保留关键字（如函数名、常数等）
     */
    private boolean isReservedKeyword(String param) {
        Set<String> keywords = new HashSet<>(Arrays.asList(
                "sin", "cos", "tan", "log", "ln", "sqrt", "abs",
                "max", "min", "sum", "product", "pi", "e"
        ));
        return keywords.contains(param.toLowerCase());
    }

 // ===== 新的测试用例 main 方法 =====
//   public static void main(String[] args) {
//       LaTeXConverter converter = new LaTeXConverter();
//
//       // 定义一个总参数列表
//       List<String> totalParams = Arrays.asList("a", "b", "c", "V", "m", "x", "y", "z");
//
//       // --- 测试场景1: 所有公式都正确 ---
//       System.out.println("--- 场景1: 所有公式都正确 ---");
//       List<String> formulas1 = Arrays.asList(
//               "x = a + b",
//               "y = V / m",
//               "z = (a * b) / c"
//       );
//       System.out.println("总参数: " + totalParams);
//       System.out.println("待校验公式: " + formulas1);
//       String result1 = converter.verifyFormulasAgainstParamList(totalParams, formulas1);
//       System.out.println("校验结果: " + result1); // 期望: true
//       System.out.println("----------------------------------------\n");
//
//
//       // --- 测试场景2: 中间某个公式有未定义的参数 ---
//       System.out.println("--- 场景2: 中间某个公式有未定义的参数 ---");
//       List<String> formulas2 = Arrays.asList(
//               "x = a + b",        // 正确
//               "y = V / k",        // 错误: k 未定义
//               "z = (a * b) / c"   // 这个不会被校验到
//       );
//       System.out.println("总参数: " + totalParams);
//       System.out.println("待校验公式: " + formulas2);
//       String result2 = converter.verifyFormulasAgainstParamList(totalParams, formulas2);
//       System.out.println("校验结果: " + result2); // 期望: 指出公式2中的k是未定义参数
//       System.out.println("----------------------------------------\n");
//
//
//       // --- 测试场景3: 最后一个公式有未定义的参数 ---
//       System.out.println("--- 场景3: 最后一个公式有未定义的参数 ---");
//       List<String> formulas3 = Arrays.asList(
//               "x = a + b",            // 正确
//               "y = V / m",            // 正确
//               "result = x * y + p"    // 错误: p 未定义
//       );
//       System.out.println("总参数: " + totalParams);
//       System.out.println("待校验公式: " + formulas3);
//       String result3 = converter.verifyFormulasAgainstParamList(totalParams, formulas3);
//       System.out.println("校验结果: " + result3); // 期望: 指出公式3中的p是未定义参数
//       System.out.println("----------------------------------------\n");
//
//       // --- 测试场景4: 公式使用了保留字 ---
//       System.out.println("--- 场景4: 公式使用了保留字 (应该被忽略, 不算作参数) ---");
//       List<String> formulas4 = Arrays.asList(
//               "x = a + sin(b)" // sin是保留字, 只有a和b是参数
//       );
//       System.out.println("总参数: " + totalParams);
//       System.out.println("待校验公式: " + formulas4);
//       String result4 = converter.verifyFormulasAgainstParamList(totalParams, formulas4);
//       System.out.println("校验结果: " + result4); // 期望: true, 因为sin不算参数, a和b都在列表中
//       System.out.println("----------------------------------------\n");
//
//       // --- 测试场景5: 总参数列表格式错误 ---
//       System.out.println("--- 场景5: 总参数列表格式错误 ---");
//       List<String> invalidTotalParams = Arrays.asList("a", "b", "1c"); // 1c格式错误
//       List<String> formulas5 = Arrays.asList("x = a + b");
//       System.out.println("总参数: " + invalidTotalParams);
//       System.out.println("待校验公式: " + formulas5);
//       String result5 = converter.verifyFormulasAgainstParamList(invalidTotalParams, formulas5);
//       System.out.println("校验结果: " + result5); // 期望: 指出总参数列表格式错误
//       System.out.println("----------------------------------------\n");
//   }
}
