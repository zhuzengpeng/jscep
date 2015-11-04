## Introduction ##

The server project provides an abstract servlet (called ScepServlet) for the handling of SCEP messages.  In order to write a functioning SCEP server, this servlet should be extended to implement the abstract methods. For more details, please see the servlet [API documentation](http://jscep.googlecode.com/svn/trunk/docs/org/jscep/server/ScepServlet.html).

## Deployment ##

There are no special deployment requirements for the ScepServlet, but you are recommended to use a servlet-mapping ending with "pkiclient.exe" (as required by the SCEP specification), for example:

```
<servlet>
    <servlet-name>scep</servlet-name>
    <servlet-class>org.example.scep.ConcreteScepServlet</servlet-class>
</servlet>

<servlet-mapping>
    <servlet-name>scep</servlet-name>
    <url-pattern>/pkiclient.exe</url-pattern>
</servlet-mapping>
```