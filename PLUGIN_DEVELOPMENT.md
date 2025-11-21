# 插件开发指南（Plugin Development Guide）

本文档指导你为 Funny Toolbox 平台开发插件，并介绍插件打包、加载与调试方式。文档基于当前版本的宿主应用（本仓库）。

重要变更：从本版本起，已移除对在 plugin.yml 中直接使用字符串类型 icon 的支持（例如 icon: "🔧" 不再允许）。请使用结构化的 icon 对象配置，见下文。

## 1. 插件是什么

插件以独立 JAR 的形式存在，内部包含：
- 插件元数据文件：META-INF/plugin.yml
- 插件主类（实现接口 com.hxuanyu.toolbox.plugin.api.IPlugin）
- 可选的后端 API（Spring Web 控制器）
- 可选的静态资源（前端页面、图片、JS/CSS 等）

宿主应用会在启动或运行时扫描插件目录，读取每个 JAR 的 plugin.yml，加载主类，注册 API 路由和静态资源，并在平台菜单中展示插件入口。

## 2. 目录结构与必需文件

典型的插件 JAR 内部结构如下：

```
your-plugin.jar
├─ META-INF/
│  └─ plugin.yml
├─ your/package/path/
│  └─ YourMainPlugin.class   (实现 IPlugin)
└─ static/                    (可选，前端静态资源)
   ├─ index.html
   └─ ...
```

## 3. plugin.yml 规范

plugin.yml 是插件的“描述符”，用于向宿主声明插件的基本信息、入口类、前端和 API 配置等。

示例（推荐完整写法）：

```yaml
id: secret-capsule
name: 秘密胶囊
version: 1.0.0
description: 一个示例插件，展示前端页面与简单 API。
author: Alice

# 图标仅支持对象形式（不再支持字符串）
icon:
  # type 只能是以下之一：emoji | url | svg | font_awesome | material | custom
  # 也可使用 framework 作为别名（等价于 type）
  type: emoji
  value: "🔐"
  # 可选：颜色、风格（前端可用）
  color: "#6633ff"
  style: "outlined"

mainClass: com.example.secretcapsule.SecretCapsulePlugin

# 前端资源配置（可选）
frontend:
  entry: /index.html       # 插件首页，相对 JAR 内 static 目录
  basePath: /static        # 固定为 /static 或你的静态资源根路径

# 后端 API 配置（可选）
api:
  prefix: /api/secret-capsule

# 可选：依赖与权限（按需扩展）
dependencies: []
permissions: []
```

icon 对象的更多例子：

- Emoji
  ```yaml
  icon:
    type: emoji
    value: "🧰"
  ```

- 远程/本地 URL（PNG/SVG 等）
  ```yaml
  icon:
    type: url
    value: https://example.com/icon.png
  ```

- 内联 SVG
  ```yaml
  icon:
    type: svg
    value: "<svg viewBox=\"0 0 24 24\" ...>...</svg>"
  ```

  或使用 data URI（未显式 type 时会智能推断）：
  ```yaml
  icon:
    value: "data:image/svg+xml;base64,...."
  ```

- Font Awesome
  ```yaml
  icon:
    type: font_awesome
    value: "fa-solid fa-wrench"
  ```

- Material Icons
  ```yaml
  icon:
    type: material
    value: "home"
  ```

注意：字符串直填 icon 已废弃（icon: "🔧" 这种写法不再解析）。若未提供 icon 或解析结果为空，宿主会在菜单/DTO 里使用默认图标。

## 4. 开发主类（实现 IPlugin）

插件主类需要实现接口：`com.hxuanyu.toolbox.plugin.api.IPlugin`。

典型实现：

```java
package com.example.secretcapsule;

import com.hxuanyu.toolbox.plugin.api.IPlugin;
import com.hxuanyu.toolbox.plugin.api.PlatformContext;

public class SecretCapsulePlugin implements IPlugin {
    @Override
    public void onLoad(PlatformContext context) {
        // 插件被加载（仍未启用）。可以做初始化、读取配置等。
    }

    @Override
    public void onEnable(PlatformContext context) {
        // 插件启用。此时可向平台注册你的路由、菜单、资源等（宿主已自动处理大部分）。
    }

    @Override
    public void onDisable(PlatformContext context) {
        // 插件被禁用或卸载，做清理工作。
    }
}
```

确保将主类的全限定名写入 plugin.yml 的 `mainClass` 字段。

## 5. 暴露后端 API（可选）

如果你的插件需要后端接口：
- 在插件工程中使用 Spring Web 注解（@RestController 或 @Controller + @RequestMapping）。
- 在 plugin.yml 的 `api.prefix` 中声明你的 API 前缀（例如 /api/secret-capsule）。
- 宿主会扫描插件包下的控制器，并按前缀注册到平台。

示例：

```java
@RestController
@RequestMapping("/hello")
public class HelloController {
    @GetMapping
    public String hello() { return "hello"; }
}
```

如果 plugin.yml 中设置了 `api.prefix: /api/secret-capsule`，则最终访问路径类似：
```
/api/secret-capsule/hello
```

## 6. 前端与静态资源（可选）

将前端构建产物（index.html、JS/CSS、图片等）放入插件 JAR 的 `/static` 目录。
在 plugin.yml 中：
- `frontend.entry` 指定首页（如 /index.html）
- `frontend.basePath` 一般为 `/static`

宿主会将资源映射到：
```
/plugins/{pluginId}/...
```

菜单入口会指向：
```
/plugin/{pluginId}
```

## 7. 打包与发布

建议使用 Maven/Gradle 构建插件工程，确保最终产物是一个包含 META-INF/plugin.yml 的 JAR。

关键点：
- 将 `plugin-api` 作为编译依赖（compileOnly 或 provided 更合适），避免将宿主 API 打包进插件。
- 确保 JAR 内存在 `META-INF/plugin.yml`。
- 若使用第三方库，请合理选择打包策略（Shade 或在宿主可见的 ClassLoader 下可加载）。

## 8. 安装与调试

1. 将插件 JAR 放入宿主应用的插件目录（通常在 `plugins/`）。
2. 启动宿主应用（或在运行中使用平台提供的加载机制）。
3. 查看控制台日志确认加载、启用是否成功。
4. 访问平台菜单，检查前端入口、图标与 API 是否可用。

若需要卸载或重载，请使用平台提供的相关接口或管理界面（若有）。

## 9. 常见问题（FAQ）

- Q: 之前的 `icon: "🔧"` 配置还能用吗？
  - A: 不能。请改为：
    ```yaml
    icon:
      type: emoji
      value: "🔧"
    ```

- Q: 不配置 icon 会怎样？
  - A: 菜单和 DTO 会使用平台默认图标。

- Q: icon 的值什么时候会加前缀？
  - A: 宿主在生成前端可识别的字符串时会按类型加前缀：
    - Font Awesome: `fa:` + value
    - Material Icons: `md:` + value
    - SVG: 非 URL/data 的内联内容将加 `svg:` 前缀

## 10. 最小可用示例

plugin.yml：

```yaml
id: hello-world
name: Hello World
version: 1.0.0
description: 最小可用示例
author: Demo
icon:
  type: emoji
  value: "👋"
mainClass: com.example.helloworld.HelloWorldPlugin
frontend:
  entry: /index.html
  basePath: /static
api:
  prefix: /api/hello
```

Java 主类：

```java
public class HelloWorldPlugin implements com.hxuanyu.toolbox.plugin.api.IPlugin {
    public void onLoad(com.hxuanyu.toolbox.plugin.api.PlatformContext context) {}
    public void onEnable(com.hxuanyu.toolbox.plugin.api.PlatformContext context) {}
    public void onDisable(com.hxuanyu.toolbox.plugin.api.PlatformContext context) {}
}
```

---

如有更多问题，欢迎阅读源码中以下位置以了解宿主侧行为：
- app/src/main/java/com/hxuanyu/funnytoolbox/plugin/core/PluginManager.java（插件加载与菜单/DTO 生成）
- app/src/main/java/com/hxuanyu/funnytoolbox/plugin/model/PluginDescriptor.java（描述符解析与图标解析）
- app/src/main/java/com/hxuanyu/funnytoolbox/plugin/registry/*（资源、路由与菜单注册）
