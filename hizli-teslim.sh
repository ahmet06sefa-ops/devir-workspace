#!/bin/bash
# TEK-ÇAĞRI sürüm zinciri (v2 hız protokolü):
# debug sınıfları + JVM testleri + release (R8) aynı Gradle oturumunda.
# Ayrı derle.sh / test.sh / derle-release.sh çağrıları YEDEK yöntemdir.
export JAVA_HOME=/opt/jdk17 ANDROID_HOME=/opt/android-sdk ANDROID_SDK_ROOT=/opt/android-sdk
export GRADLE_USER_HOME=/home/user/.gradle-home
export PATH=$JAVA_HOME/bin:/opt/gradle-8.7/bin:$PATH
cd /home/user/GunlukAsistan || exit 1
gradle :app:testDebugUnitTest :app:assembleRelease --no-daemon --console=plain > /home/user/hizli.log 2>&1
echo "EXIT=$?" >> /home/user/hizli.log
