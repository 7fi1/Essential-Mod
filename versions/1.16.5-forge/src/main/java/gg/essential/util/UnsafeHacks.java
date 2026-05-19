/*
 * Copyright (c) 2024 ModCore Inc. All rights reserved.
 *
 * This code is part of ModCore Inc.'s Essential Mod repository and is protected
 * under copyright registration # TX0009138511. For the full license, see:
 * https://github.com/EssentialGG/Essential/blob/main/LICENSE
 *
 * You may not use, copy, reproduce, modify, sell, license, distribute,
 * commercialize, or otherwise exploit, or create derivative works based
 * upon, this file or any other in this repository, all of which is reserved by Essential.
 */
package gg.essential.util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.function.Function;

public class UnsafeHacks {
    private static final Unsafe unsafe;
    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static <O, T> Accessor<O, T> makeAccessor(Class<? extends O> cls, String field) {
        try {
            return makeAccessor(cls.getDeclaredField(field));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static <O, T> Accessor<O, T> makeAccessor(Field field) {
        if (field.getType().isPrimitive()) {
            throw new UnsupportedOperationException("Only Object types are supported.");
        }
        if ((field.getModifiers() & Modifier.STATIC) != 0) {
            Object base = unsafe.staticFieldBase(field);
            long offset = unsafe.staticFieldOffset(field);
            return new Accessor<O, T>() {
                @SuppressWarnings("unchecked")
                @Override
                public T get(O owner) {
                    return (T) unsafe.getObject(base, offset);
                }

                @Override
                public void set(O owner, T value) {
                    unsafe.putObject(base, offset, value);
                }
            };
        } else {
            long offset = unsafe.objectFieldOffset(field);
            return new Accessor<O, T>() {
                @SuppressWarnings("unchecked")
                @Override
                public T get(O owner) {
                    return (T) unsafe.getObject(owner, offset);
                }

                @Override
                public void set(O owner, T value) {
                    unsafe.putObject(owner, offset, value);
                }
            };
        }
    }

    public interface Accessor<O, T> {
        T get(O owner);
        void set(O owner, T value);

        default void update(O owner, Function<T, T> func) {
            set(owner, func.apply(get(owner)));
        }
    }
}
