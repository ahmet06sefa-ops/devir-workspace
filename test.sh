#!/bin/bash
export JAVA_HOME=/opt/jdk17 ANDROID_HOME=/opt/android-sdk ANDROID_SDK_ROOT=/opt/android-sdk
export GRADLE_USER_HOME=/home/user/.gradle-home
export PATH=$JAVA_HOME/bin:/opt/gradle-8.7/bin:$PATH
export GRADLE_OPTS=""
cd /home/user/GunlukAsistan
gradle :app:testDebugUnitTest --console=plain > /home/user/test.log 2>&1
echo "EXIT=$?" >> /home/user/test.log
