LastaFlute
=======================
Typesafe Web Framework for LeAn STArtup with DBFlute and Java8

- for Lean Startup and Incremental Development
- making the fullest possible use of Java8

## Rhythm & Speed Programming
- Hot Deploy
- Lightening Boot

## Adaptable-to-Change Programming
- Hard Typesafe
- Aggressive DB Change
- Crazy Logging

## Naturally-be-so Programming
- Convention Mapping
- Less Choice
- Default Libraries

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

# Quick Trial
Can boot it by example of LastaFlute:

1. git clone https://github.com/dbflute-session/lastaflute-example-harbor.git
2. prepare database by *ReplaceSchema at DBFlute client directory 'dbflute_maihamadb'  
3. compile it by Java8, on e.g. Eclipse or IntelliJ or ... as Maven project
4. execute the *main() method of (org.docksidestage.boot) HarborBoot
5. access to http://localhost:8090/harbor  
and login by user 'Pixy' and password 'sea', and can see debug log at console.

*ReplaceSchema
```java
// call manage.sh at lastaflute-example-harbor/dbflute_maihamadb
// and select replace-schema in displayed menu
...:dbflute_maihamadb ...$ sh manage.sh
```

*main() method
```java
public class HarborBoot {

    public static void main(String[] args) {
        new JettyBoot(8090, "/harbor").asDevelopment().bootAwait();
    }
}
```

# Information
## Maven Dependency in pom.xml
```xml
<dependency>
    <groupId>org.lastaflute</groupId>
    <artifactId>lastaflute</artifactId>
    <version>0.6.0-RCL</version>
</dependency>
```

## License
Apache License 2.0

## Official site
(English pages have a low count but are increscent...)  
http://dbflute.seasar.org/lastaflute/

# Thanks, Framewoks
LastaFlute forks SAStruts, Struts and Commons utilities and (heavily) extends it.  
And is influenced by SpringBoot, Play2.
If the frameworks were not there, no LastaFlute here.

I appreciate every framework.

# Thanks, Friends
LastaFlute is used by:  
comming soon...
