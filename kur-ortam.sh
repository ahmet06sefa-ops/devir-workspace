#!/bin/bash
# GunlukAsistan derleme ortamı kurucu.
# /opt oturum arası silindiği için her yeni oturumda bir kez çalıştır:  bash ~/kur-ortam.sh
set -e

echo "=== [1/3] JDK 17 ==="
if [ ! -x /opt/jdk17/bin/java ]; then
  cd /tmp
  curl -sL --max-time 600 -o jdk17.tar.gz \
    "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse"
  sudo mkdir -p /opt/jdk17
  sudo tar xzf jdk17.tar.gz -C /opt/jdk17 --strip-components=1
  rm -f jdk17.tar.gz
  echo "  kuruldu"
else
  echo "  zaten var"
fi

echo "=== [2/3] Android SDK ==="
if [ ! -d /opt/android-sdk/build-tools/34.0.0 ]; then
  cd /tmp
  curl -sL --max-time 600 -o cmdtools.zip \
    https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
  sudo mkdir -p /opt/android-sdk/cmdline-tools
  rm -rf /tmp/cmdx && unzip -q -o cmdtools.zip -d /tmp/cmdx
  sudo rm -rf /opt/android-sdk/cmdline-tools/latest
  sudo mv /tmp/cmdx/cmdline-tools /opt/android-sdk/cmdline-tools/latest
  sudo chown -R "$(id -u)":"$(id -g)" /opt/android-sdk
  rm -rf cmdtools.zip /tmp/cmdx
  export JAVA_HOME=/opt/jdk17
  export PATH=$JAVA_HOME/bin:$PATH
  yes | /opt/android-sdk/cmdline-tools/latest/bin/sdkmanager --licenses >/dev/null 2>&1 || true
  /opt/android-sdk/cmdline-tools/latest/bin/sdkmanager \
    "platform-tools" "platforms;android-34" "build-tools;34.0.0" >/dev/null 2>&1
  echo "  kuruldu"
else
  echo "  zaten var"
fi

echo "=== [3/3] Gradle 8.7 ==="
if [ ! -x /opt/gradle-8.7/bin/gradle ]; then
  cd /tmp
  curl -sL --max-time 600 -o gradle.zip https://services.gradle.org/distributions/gradle-8.7-bin.zip
  sudo unzip -q -o gradle.zip -d /opt
  rm -f gradle.zip
  echo "  kuruldu"
else
  echo "  zaten var"
fi

# ortam betiği
cat > /home/user/ortam.sh <<'EOF'
export JAVA_HOME=/opt/jdk17
export ANDROID_HOME=/opt/android-sdk
export ANDROID_SDK_ROOT=/opt/android-sdk
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools/34.0.0:/opt/gradle-8.7/bin:$PATH
export GRADLE_USER_HOME=/home/user/.gradle-home
export GRADLE_OPTS="-Dorg.gradle.daemon=false"
EOF
grep -q 'ortam.sh' /home/user/.bashrc 2>/dev/null || echo 'source /home/user/ortam.sh' >> /home/user/.bashrc

source /home/user/ortam.sh
echo
echo "JAVA : $(java -version 2>&1 | head -1)"
echo "SDK  : $(ls $ANDROID_HOME/platforms 2>/dev/null)"
echo "GRDL : $(gradle -v 2>&1 | grep -m1 '^Gradle')"
echo "ORTAM_HAZIR"
