# LastaFlute
Typesafe Web Framework for LeAn STArtup with DBFlute and Java8

## Example Code
```java
// e.g. ProductListAction, mapping to URL '/product/list'

@Execute
public HtmlResponse index(OptionalThing<Integer> pageNumber, ProductSearchForm form) {
    validate(form, messages -> {} , () -> {
        return asHtml(path_Product_ProductListJsp);
    });
    PagingResultBean<Product> page = selectProductPage(pageNumber.orElse(1), form);
    List<ProductSearchRowBean> beans = page.mappingList(product -> {
        return mappingToBean(product);
    });
    return asHtml(path_Product_ProductListJsp).renderWith(data -> {
        data.register("beans", beans);
        registerPagingNavi(data, page, form);
    });
}
```
## Rythem & Speed Programming
- HotDeploy
- Lightening Boot

## Adaptable-to-Change Programming
- Hard Typesafe
- Aggressive DB Change
- Crazy Logging

## Naturally-be-so Programming
- Convention URL Mapping
- Less Choice
- Default Libraries

# Japanese site (English comming soon...)
http://dbflute.seasar.org/ja/lastaflute/
