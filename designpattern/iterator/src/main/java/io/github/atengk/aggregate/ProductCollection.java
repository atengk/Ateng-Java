package io.github.atengk.aggregate;


import io.github.atengk.iterator.ProductIterator;

/**
 * 商品集合接口
 */
public interface ProductCollection {

    /**
     * 创建并返回一个商品迭代器
     */
    ProductIterator createIterator();
}
