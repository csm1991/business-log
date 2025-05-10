# Business Log 系统

## 功能介绍
- 该项目是一个业务日志组件，用于解决业务日志写入痛点问题。
- 常见业务日志写入会在service中进行埋点写入，不方便后期维护和扩展，比如入参变更、日志内容变更等场景
- 日志写入还会影响接口响应耗时，本组件只在切面进行日志参数组装，后续写入通过异步完成(本组件不包含写入，需用户自行根据系统要求实现)
- 该项目还支持比较数据操作前后的差异，减少业务逻辑代码，不需要开发人员手写查询(目前只支持主键ID查询，基于mybatis-plus的selectBatchIds方法)
- 日志切面最终只组装成BusinessLogEntity集合，如果您的系统需要国际化，本人建议直接将BusinessLogEntity对象保存，并且附带一个国际化词条key(我在我们公司的项目就是这样实施的)，然后在日志展示的地方进行二次渲染，这样方便后期改词条内容和国际化等操作

## 技术栈
- Java 17(可以自行改成JDK8，本项目没有用Java 17特有的语法)
- Spring Boot 3.4.5
- Maven 3.9+ 
- Lombok 1.18.30

## 模块说明
### log-core
日志核心模块，提供：
- 统一日志格式规范
- 日志切面自动配置
- 日志上下文传递支持

### demo-server
演示服务模块，包含：
- Spring Boot启动类
- 日志使用示例
- REST API接口演示

## 快速启动
```bash
# 全量构建
mvn clean package

# 运行演示服务
cd demo-server
mvn spring-boot:run

# 单独构建核心模块
cd ../log-core
mvn install
```

## 环境要求
- JDK 17+
- Maven 3.9+
- Git 2.30+

## 项目结构
```
business-log/
├── demo-server/     # 演示服务模块
├── log-core/        # 日志核心模块，如果需要放到你项目中使用，只需要依赖这个包即可
└── pom.xml          # 父级POM
```

## 构建指南
父级POM已包含：
- Spring Boot依赖管理
- 通用插件配置
- 模块聚合配置

## 许可证
Apache License 2.0