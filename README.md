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
## Rhythm & Speed Programming
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
Can boot it by example of LastaFlute:

1. prepare Java8 compile environment
2. clone https://github.com/dbflute-session/lastaflute-example-harbor
3. execute the main method of (org.docksidestage.boot) HarborBoot
4. access to http://localhost:8090/harbor

```java
public class HarborBoot {

    public static void main(String[] args) {
        new JettyBoot(8090, "/harbor").asDevelopment().bootAwait();
    }
}
```

Can login by user 'Pixy' and password 'sea', and can see debug log at console.

# Information
## Maven Dependency
```xml
<dependency>
    <groupId>org.lastaflute</groupId>
    <artifactId>lastaflute</artifactId>
    <version>0.6.0-RCL</version>
</dependency>
```

## License
Apache License 2.0

## Japanese site (English comming soon...)
http://dbflute.seasar.org/ja/lastaflute/

# Thanks, Framewoks
LastaFlute forks SAStruts, Struts and Commons utilities and (heavily) extends it.  
If the frameworks were not there, no LastaFlute here.

I appreciate every framework.

# Thanks, Friends
LastaFlute is used by:  
comming soon...
