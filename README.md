# LastaFlute
Typesafe Web Framework for LeAn STArtup with DBFlute and Java8

- for Lean Startup and Incremental Development
- making the fullest possible use of Java8

## Example Code
```java
// e.g. ProductListAction, mapping to URL '/product/list/'

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

# Quick Trial
1. prepare Java8 compile environment
2. clone https://github.com/dbflute-session/lastaflute-example-harbor
3. execute the main method of (org.docksidestage.boot) HarborBoot
4. access to http://localhost:8090/harbor

*you can login by user 'Pixy' and password 'sea', and can see debug log at console

# Japanese site (English comming soon...)
http://dbflute.seasar.org/ja/lastaflute/
