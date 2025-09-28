package org.raibnowsea.cache.utils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.raibnowsea.cache.exception.GuiguException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 *
 * 工具类：主要完成对数据的序列化和反序列化操作
 */
public class Jsons {


    static Logger logger = LoggerFactory.getLogger(Jsons.class);
    static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 序列化操作(接的是对象--->返回的是字符串)
     */
    public static String objToStr(Object content) {

        // 可以完成对任意数据的序列化和反序列化操作
        // @RequestBody注解的底层使用的就是ObjectMapper完成的操作---将字符串反序列化成对象
        // @ResponseBody注解的底层使用的就是ObjectMapper完成的操作---将对象转成字符串
        // 一、是个对象有数据   二、是个对象没数据 ：1.对象是个null ? 2.对象是一个Map（双列）  ?  3.对象是是一个List<单列> ？ 4.对象是是一个Set<单列> ？ 5.对象是一个数组String[]   ？
        try {
            // 1.真正的数据对应的字符串（Map("name":"zs","age":18) abc--"{"name":"zs","age":18}"） 2.没数据对应的字符串（"null" "{}" "[]"）
            String resultStr = objectMapper.writeValueAsString(content);
            return resultStr;
        } catch (JsonProcessingException e) {
            logger.error("对象：{}序列化成字符串失败，原因是{}", content, e.getMessage());
            throw new GuiguException(201, "数据在转换期间出现了序列化异常");
        }


    }


    /**
     * 反序列化操作(接的是字符串--->返回的是对象【指定的类型】)
     * 不带泛型
     *
     * @return
     */

    public static  <T>  T strToObj(String content, Class<T> tClass) {

        try {
            // "{"name":"hzk","age":18}"--Map.clas
            // "{}"---Map.class
            // "[]"---List.class Set.class  Array.class
            T t = objectMapper.readValue(content, tClass);
            return t;
        } catch (JsonProcessingException e) {
            logger.error("字符串：{}反序列化成对象失败，原因是{}", content, e.getOriginalMessage());
            throw new GuiguException(201, "数据在转换期间出现了序列化异常");
        }


    }

    /**
     * 反序列化操作(接的是字符串--->返回的是对象【指定的类型】)
     * 带泛型
     * @return
     */

    public static  <T>  T strToObj(String content, TypeReference<T> tClass) {

        try {
            // "{"name":"hzk","age":18}"--Map.clas
            // "{}"---Map.class
            // "[]"---List.class Set.class  Array.class
            T t = objectMapper.readValue(content, tClass);
            return t;
        } catch (JsonProcessingException e) {
            logger.error("字符串：{}反序列化成对象失败，原因是{}", content, e.getOriginalMessage());
            throw new GuiguException(201, "数据在转换期间出现了序列化异常");
        }


    }


    /**
     * 获取所有(不同情况对象没有数据)正则表达式规则
     * 一、是个对象有数据   二、是个对象没数据 ：1.对象是个null ? 2.对象是一个Map（双列）  ?
     * 3.对象是是一个List<单列> ？ 4.对象是是一个Set<单列> ？ 5.对象是一个数组String[]   ？
     * @return
     */
    public static List<String> getAllRegexRules() {

        ArrayList<String> list = new ArrayList<>();
        list.add("^null$");
        list.add("^\\{\\}$");
        list.add("^\\[\\]$");
        return list;
    }

    /**
     * 对应的对象数据情况的正则判断
     * @param compareContent
     * @param regexRule
     * @return
     */
    public static Boolean isMath(String compareContent, String regexRule) {

        return Pattern.matches(regexRule, compareContent);
    }


}
