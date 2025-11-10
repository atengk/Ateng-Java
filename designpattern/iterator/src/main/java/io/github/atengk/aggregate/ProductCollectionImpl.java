package io.github.atengk.aggregate;


import io.github.atengk.iterator.ProductIterator;
import io.github.atengk.iterator.ProductIteratorImpl;
import io.github.atengk.model.Product;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 商品集合实现类
 */
@Component
public class ProductCollectionImpl implements ProductCollection {

    private final List<Product> productList = new ArrayList<>();

    /**
     * 添加商品
     */
    public void addProduct(Product product) {
        productList.add(product);
    }

    /**
     * 获取所有商品
     */
    public List<Product> getAllProducts() {
        return productList;
    }

    /**
     * 创建一个新的商品迭代器
     */
    @Override
    public ProductIterator createIterator() {
        return new ProductIteratorImpl(productList);
    }
}
