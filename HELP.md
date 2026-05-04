# Getting Started

### Reference Documentation
For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/2.7.3/maven-plugin/reference/html/)
* [Create an OCI image](https://docs.spring.io/spring-boot/docs/2.7.3/maven-plugin/reference/html/#build-image)
* [Spring Web](https://docs.spring.io/spring-boot/docs/2.7.3/reference/htmlsingle/#web)

### Guides
The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)

### Form Link
https://forms.gle/C3NJt591sNxrXU3VA

## Known Issue — homelab deployment

On startup, Hibernate logs two WARNs:

    alter table homes modify column first_info_text tinyblob — Data too long
    alter table homes modify column first_info_text_short tinyblob — Data too long

**Cause:** The JPA entity maps these fields to `tinyblob` but the DB column is `LONGBLOB`
(from the original Liquibase migration). With `ddl-auto=update`, Hibernate tries to shrink
them and fails because existing data is too large.

**App is not broken** — the columns stay as LONGBLOB and everything works fine.

**Fix:** In the `Homes` entity, annotate both fields with:

    @Column(columnDefinition = "LONGBLOB")

This makes Hibernate match the actual DB type and stops the WARN on every startup.
