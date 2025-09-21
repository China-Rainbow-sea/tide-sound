package com.rainbowsea.tidesound.common.config.date;


import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
import com.fasterxml.jackson.databind.ser.std.DateSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rainbowsea.tidesound.common.execption.GuiguException;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * 自定义时间上的序列化和反序列化器
 * 用于统一前后端的 Date 时间对象，为中国时区的时间
 */


@Configuration
public class CustomerDateHandler implements Jackson2ObjectMapperBuilderCustomizer {
    // 自定义处理
    @Override
    public void customize(Jackson2ObjectMapperBuilder jacksonObjectMapperBuilder) {

        // 1.创建时间模版对象
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        // 2.对时间格式进行序列化
        javaTimeModule.addSerializer(Date.class,new DateSerializer(){

            // 自定义对时间类型做序列化操作

            /**
             * param1:指的是要序列化的对象里面Date类型的字段以及值
             * param2:json生成器：用来负责将数据输出到json中去。
             * pram3:暂时用不到
             * @param value Value to serialize; can <b>not</b> be null.
             * @param g Generator used to output resulting Json content
             * @param provider Provider that can be used to get serializers for
             *   serializing Objects value contains, if any.
             * @throws IOException
             */
            @Override
            public void serialize(Date value, JsonGenerator g, SerializerProvider provider) throws IOException {
//                super.serialize(value, g, provider);
//                 处理时间时区 处理日期时间格式
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+8"));
                String format = simpleDateFormat.format(value);   // 字符串
                g.writeString(format);
            }
        });


        // 3.对时间格式进行反序列化
        javaTimeModule.addDeserializer(Date.class,new DateDeserializers.DateDeserializer(){

            /**
             * param1:json解析器:用来解析json数据
             * @param p Parsed used for reading JSON content
             * @param ctxt Context that can be used to access information about
             * this deserialization activity.
             *
             * @return
             * @throws IOException
             */
            @Override
            public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
//                return super.deserialize(p, ctxt);
                String timeStr = p.getText();


                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+8"));
                try {
                    Date parseDate = simpleDateFormat.parse(timeStr);
                    return  parseDate;
                } catch (ParseException e) {

                    throw new GuiguException(201,"时间格式不能，不能解析");
                }
            }
        });

        // 4. 将时间模式注册到builder中
        jacksonObjectMapperBuilder.modulesToInstall(javaTimeModule);

    }
}
