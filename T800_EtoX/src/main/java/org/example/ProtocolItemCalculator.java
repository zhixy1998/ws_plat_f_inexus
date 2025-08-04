package org.example;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ProtocolItemCalculator {

   public static boolean isNumeric(String str) {
       try{
           Double.parseDouble(str); //尝试将字符串转换为数字，“123或12.3”成功，“abc”失败
           return true;
       }catch (NumberFormatException e){
           return false;
       }
   }

   public static Object eval(String expression) {
        try{
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("JavaScript");
            return engine.eval(expression);
        }catch (Exception e){
            System.err.println("表达式错误" + expression + ":" + e.getMessage());
            return null;
        }
   }

   public static String getEquationValue(String equationStr, Map<String, String>dicInput,String[] keyOut) {
       String strResult = equationStr;
       for (String func : new String[]{"TRUNC", "MOD"}) {
           if (equationStr.contains(func)) {
               for (String item : dicInput.keySet()) {
                   if (equationStr.contains(item)) {
                       double result = func.equals("TRUNC")
                               ? trunc(getTerm(func, strResult), dicInput, item)
                               : mod(getTerm(func, strResult), dicInput, item);
                       strResult = strResult.replace(getTerm(func, strResult), String.valueOf(result));
                       String finalResult = String.valueOf(eval(strResult));
                       keyOut[0] = item;
                       return finalResult;
                   }
               }
           }
       }

       for (String item : dicInput.keySet()) {
           if (equationStr.contains(item)) {
               strResult = strResult.replace(item, dicInput.get(item));
               keyOut[0] = item;
               return String.valueOf(eval(strResult));
           }
       }

       keyOut[0] = null;//明确无变量匹配
       return "";
   }

   public static double trunc(String str, Map<String, String> dicInput, String item) {
       String inside = str.substring(6, str.length() - 1);
       String strResult = inside.replace(item, dicInput.get(item));
       Object value = eval(strResult);
       return value == null ? 0 : Math.floor(Double.parseDouble(value.toString()));
   }

   public static double mod(String str, Map<String, String> dicInput, String item) {
       String inside = str.substring(4,str.length() - 1);
       String strResult = inside.replace(item, dicInput.get(item));
       String[] term = strResult.split(",");
       Object left = eval(term[0]);
       Object right = eval(term[1]);
       double l = left == null ? 0 : Double.parseDouble(left.toString());
       double r = right == null ? 1 : Double.parseDouble(right.toString());
       return l % r;
   }

   public static String getTerm(String key, String str) {
       int index = str.indexOf(key); //定位key第一次出现的位置
       String strLast = str.substring(index);
       int p = 0, q = 0, pos = 0;
       for(int i = 0; i < strLast.length(); i++){
           if (strLast.charAt(i) == '(') p++;
           if(strLast.charAt(i) == ')') {
               q++;
               if(p == q) {
                   pos = i + 1;
                   break;
               }
           }
       }
       return strLast.substring(0, pos);
   }

    public static byte[] toBytes(String inputValue, int bytesLength, String objType) {
        byte[] lastByte = new byte[bytesLength];
        if (objType == null) {
            System.err.println("[错误] objType 为 null，inputValue = " + inputValue);
            return lastByte;
        }
        switch (objType) {
            case "0":
                lastByte = new byte[]{(byte) Double.parseDouble(inputValue)};
                break;
            case "1":
                int num1 = Integer.parseInt(inputValue);
                byte[] getting1 = ByteBuffer.allocate(4).putInt(num1).array();
                getNewBytes(getting1, lastByte, bytesLength);
                break;
            case "2":
                float num2 = Float.parseFloat(inputValue);
                byte[] getting2 = ByteBuffer.allocate(4).putFloat(num2).array();
                getNewBytes(getting2, lastByte, bytesLength);
                break;
            case "3":
                byte[] getting3 = inputValue.getBytes(StandardCharsets.UTF_8);
                getNewBytes(getting3, lastByte, bytesLength);
                break;
            default:
                System.err.println("[错误] 未识别的 objType: " + objType);
                break;
        }
        return lastByte;
    }

    private static byte[] getNewBytes(byte[] source, byte[] lastByte, int length) {
        int startIndex = source.length - length;
        for (int i = 0; i < lastByte.length; i++) {
            if ((startIndex + i) < source.length) {
                lastByte[i] = source[startIndex + i];
            } else {
                lastByte[i] = 0;
            }
        }
        return lastByte;
    }
}
