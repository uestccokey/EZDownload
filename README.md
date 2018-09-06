# EZDownload

一款轻量级，易扩展的Android文件下载框架
支持智能多线程分块下载（类似迅雷），支持线程池，支持断点续传，支持暂停恢复

[ ![Download](https://api.bintray.com/packages/uestccokey/maven/EZDownload/images/download.svg) ](https://bintray.com/uestccokey/maven/EZDownload/_latestVersion)

### Usage

#### Gradle
``` gradle
android {
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}

dependencies {
    compile 'cn.ezandroid:EZDownload:{Latest version}' // Gradle version < 3.0
    // or
    implementation 'cn.ezandroid:EZDownload:{Latest version}' // Gradle version >= 3.0
}
```


