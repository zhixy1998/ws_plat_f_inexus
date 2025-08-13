package com.persee.workstation.uv_inexus.service.parseReport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.mariuszgromada.math.mxparser.Expression;

public class LaTeXConverter {

    /**
     * 验证LaTeX公式并检查参数是否与列表完全匹配（不多不少）
     * @param paramList 允许的参数列表
     * @param latexFormula LaTeX公式字符串
     * @return "true" 如果公式有效且参数完全匹配；否则返回错误信息
     */
    public String verifyAndConvert(List<String> paramList, String latexFormula) {
        try {
            // 0. 校验参数列表格式
            String paramFormatError = validateParameterFormat(paramList);
            if (paramFormatError != null) {
                return paramFormatError;
            }

            // 1. 将LaTeX转换为普通表达式
            String expression = convertLaTeXToExpression(latexFormula);
            if (expression == null) {
                return "公式转换失败: 不支持的LaTeX语法";
            }

            // 2. 验证表达式是否有效
            if (!isValidExpression(expression)) {
                return "公式无效: " + expression;
            }

            // 3. 提取等号右边的表达式
            String rightHandSide = extractRightHandSide(expression);
            System.out.println("提取的变量: " + rightHandSide);

            if (rightHandSide == null) {
                return "公式格式错误: 缺少等号";
            }

            // 4. 提取并检查参数（完全匹配：数量和内容都必须一致）
            Set<String> formulaParams = extractParameters(rightHandSide);
            Set<String> paramSet = paramList.stream()
                    .map(String::trim)
                    .collect(Collectors.toSet());

            // 检查是否有多余的参数
            List<String> extraParams = formulaParams.stream()
                    .filter(param -> !paramSet.contains(param))
                    .collect(Collectors.toList());

            // 检查是否有缺失的参数
            List<String> missingParams = paramSet.stream()
                    .filter(param -> !formulaParams.contains(param))
                    .collect(Collectors.toList());

            // 构建错误信息
            if (!extraParams.isEmpty() || !missingParams.isEmpty()) {
                StringBuilder errorMsg = new StringBuilder("参数不匹配: ");
                if (!extraParams.isEmpty()) {
                    errorMsg.append("公式包含多余参数: ").append(String.join(", ", extraParams));
                }
                if (!missingParams.isEmpty()) {
                    if (!extraParams.isEmpty()) {
                        errorMsg.append("; ");
                    }
                    errorMsg.append("公式缺少参数: ").append(String.join(", ", missingParams));
                }
                return errorMsg.toString();
            }

            return "true";
        } catch (Exception e) {
            return "处理公式时出错: " + e.getMessage();
        }
    }

    /**
     * 校验参数列表格式，必须是纯字母或字母+数字
     * @param paramList 参数列表
     * @return 错误信息，如果格式正确则返回null
     */
    private String validateParameterFormat(List<String> paramList) {
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
        // 处理左括号: \left( → (
        expr = expr.replaceAll("\\\\left\\(", "(");
        // 处理右括号: \right) → )
        expr = expr.replaceAll("\\\\right\\)", ")");
        // 清理剩余的反斜杠
        expr = expr.replaceAll("\\\\", "");

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
     * 将变量替换为默认值
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
     * 检查是否为保留关键字
     */
    private boolean isReservedKeyword(String param) {
        Set<String> keywords = new HashSet<>(Arrays.asList(
                "sin", "cos", "tan", "log", "ln", "sqrt", "abs",
                "max", "min", "sum", "product", "pi", "e"
        ));
        return keywords.contains(param.toLowerCase());
    }

    // 测试方法
    public static void main(String[] args) {
        LaTeXConverter converter = new LaTeXConverter();
        
        // 测试1: 参数完全匹配
        String latex1 = "x = a * V * V2 * 10^(-4) / (m * V1)";
        List<String> params1 = Arrays.asList("a", "V", "V2", "m", "V1");
        System.out.println("测试1结果: " + converter.verifyAndConvert(params1, latex1)); 
        
        // 测试2: 公式参数多于列表
        String latex2 = "x = a * V * V2 * b / (m * V1)"; // 多了b
        List<String> params2 = Arrays.asList("a", "V", "V2", "m", "V1");
        System.out.println("测试2结果: " + converter.verifyAndConvert(params2, latex2)); 
        
        // 测试3: 公式参数少于列表
        String latex3 = "x = a * V / (m * V1)"; // 少了V2
        List<String> params3 = Arrays.asList("a", "V", "V2", "m", "V1");
        System.out.println("测试3结果: " + converter.verifyAndConvert(params3, latex3)); 
        
        // 测试4: 两者参数完全不同
        String latex4 = "x = p * q";
        List<String> params4 = Arrays.asList("a", "b");
        System.out.println("测试4结果: " + converter.verifyAndConvert(params4, latex4)); 
    }
}
