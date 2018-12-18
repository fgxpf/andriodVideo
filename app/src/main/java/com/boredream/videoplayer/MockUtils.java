package com.boredream.videoplayer;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 数据工具类
 */
public class MockUtils {

    public static <T> T mockData(Class<T> clazz, String path) {
        T t = null;
        try {
            t = clazz.newInstance();
            setValue(t, clazz, path);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return t;
    }

    private static <T> void setValue(Object object, Class<T> clazz, String path) throws IllegalAccessException {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (Modifier.isFinal(field.getModifiers()) || Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            Object value = getFieldMockValue(field, path);
            if (value != null) {
                field.setAccessible(true);
                field.set(object, value);
            }
        }
    }

    /**
     * 模拟基础类型数据
     */
    private static Object getFieldMockValue(Field field, String path) {
        Class clazzType = field.getType();
        Object value = null;
        if (clazzType == int.class || clazzType == Integer.class) {
            value = new Random().nextInt(100);
        }
        else if (clazzType == long.class || clazzType == Long.class) {
            value = Math.abs(new Random().nextLong() % 10000);
        }
        else if (clazzType == float.class || clazzType == Float.class) {
            value = new Random().nextFloat();
        }
        else if (clazzType == double.class || clazzType == Double.class) {
            value = new Random().nextDouble();
        }
        else if (clazzType == String.class) {
            value = path;
        }
        else if (clazzType == boolean.class || clazzType == Boolean.class) {
            value = new Random().nextBoolean();
        }
        return value;
    }

}
