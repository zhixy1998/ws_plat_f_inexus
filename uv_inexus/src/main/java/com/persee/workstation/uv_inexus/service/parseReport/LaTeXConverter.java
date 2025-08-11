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
     * 验证LaTeX公式并检查参数是否存在于列表中
     * @param paramList 允许的参数列表
     * @param latexFormula LaTeX公式字符串
     * @return "true" 如果公式有效且所有参数都在列表中；否则返回错误信息
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

            // 4. 提取并检查参数
            Set<String> formulaParams = extractParameters(rightHandSide);
            Set<String> paramSet = paramList.stream()
                    .map(String::trim)
                    .collect(Collectors.toSet());

            List<String> missingParams = formulaParams.stream()
                    .filter(param -> !paramSet.contains(param))
                    .collect(Collectors.toList());

            if (!missingParams.isEmpty()) {
                return "公式中参数不存在: " + String.join(", ", missingParams);
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
        String latex = "x=a*V*V2*10^(-4)/(m*V1)";

        // 测试合法参数
        List<String> validParams = Arrays.asList("V1", "V2", "V3", "V4", "V6");
        String result = converter.verifyAndConvert(validParams, latex);
        System.out.println("验证结果: " + result);

        // 测试非法参数
        List<String> invalidParams = Arrays.asList("V1", "2V", "V_2", "123");
        result = converter.verifyAndConvert(invalidParams, latex);
        System.out.println("验证结果: " + result);
    }
}
