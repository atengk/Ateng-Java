package io.github.atengk.factory;

import io.github.atengk.product.Button;
import io.github.atengk.product.TextField;

/**
 * 抽象工厂接口
 *
 * <p>用于创建一系列相关产品（按钮、输入框等）。</p>
 */
public interface UIFactory {
    /**
     * 创建按钮
     *
     * @return Button 实例
     */
    Button createButton();

    /**
     * 创建输入框
     *
     * @return TextField 实例
     */
    TextField createTextField();
}
