pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 1. 添加阿里云公共仓库 (替代 mavenCentral)
        maven { url = uri("https://maven.aliyun.com/repository/public") }

        // 2. 添加阿里云 Google 仓库 (替代 google)
        maven { url = uri("https://maven.aliyun.com/repository/google") }

        // 3. 添加阿里云 Gradle 插件仓库 (替代 gradlePluginPortal)
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }

        // 4. 保留原有的作为兜底 (可选，但建议保留以防万一)
        google()
        mavenCentral()
    }
}
rootProject.name = "MobileMusic"
include(":app")
