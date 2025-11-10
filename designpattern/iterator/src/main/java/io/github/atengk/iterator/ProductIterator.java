package io.github.atengk.iterator;

import io.github.atengk.model.Product;

/**
 * 商品迭代器接口
 */
public interface ProductIterator {

    /**
     * 判断是否还有下一个商品
     */
    boolean hasNext();

    /**
     * 获取下一个商品
     */
    Product next();
}