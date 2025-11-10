package io.github.atengk.iterator;


import io.github.atengk.model.Product;

import java.util.List;

/**
 * 商品迭代器实现类
 */
public class ProductIteratorImpl implements ProductIterator {

    private final List<Product> productList;
    private int position = 0;

    public ProductIteratorImpl(List<Product> productList) {
        this.productList = productList;
    }

    @Override
    public boolean hasNext() {
        return position < productList.size();
    }

    @Override
    public Product next() {
        if (!hasNext()) {
            throw new IllegalStateException("没有更多商品了");
        }
        return productList.get(position++);
    }
}
