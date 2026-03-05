FROM ubuntu:22.04

ARG USER_NAME=developer
ARG USER_UID=1000
ARG USER_GID=1000

ENV DEBIAN_FRONTEND=noninteractive
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH="${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${ANDROID_HOME}/build-tools/35.0.0:${PATH}"
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

# --- System packages ---
RUN apt-get update && apt-get install -y --no-install-recommends \
    openjdk-17-jdk \
    curl wget unzip git \
    libx11-6 libxext6 libxrender1 libxtst6 libxi6 \
    libgl1-mesa-glx libglu1-mesa \
    libpulse0 libasound2 \
    ca-certificates gnupg \
    adb \
    && rm -rf /var/lib/apt/lists/*

# --- Android SDK command-line tools ---
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O /tmp/cmdtools.zip && \
    unzip -q /tmp/cmdtools.zip -d /tmp/cmdtools && \
    mv /tmp/cmdtools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest && \
    rm -rf /tmp/cmdtools /tmp/cmdtools.zip

# --- Accept licenses and install SDK components ---
RUN yes | sdkmanager --licenses > /dev/null 2>&1 && \
    sdkmanager \
        "platform-tools" \
        "build-tools;35.0.0" \
        "platforms;android-35" \
        "platforms;android-33" \
        "extras;android;m2repository" \
        "extras;google;m2repository"

# --- IntelliJ IDEA Community (x86_64) ---
RUN wget -q https://download.jetbrains.com/idea/ideaIC-2024.3.4.tar.gz -O /tmp/idea.tar.gz && \
    tar -xzf /tmp/idea.tar.gz -C /opt && \
    mv /opt/idea-IC-* /opt/idea && \
    ln -s /opt/idea/bin/idea.sh /usr/local/bin/idea.sh && \
    rm /tmp/idea.tar.gz

# --- Create dev user ---
RUN groupadd -g ${USER_GID} ${USER_NAME} 2>/dev/null || true && \
    useradd -m -u ${USER_UID} -g ${USER_GID} -s /bin/bash ${USER_NAME} && \
    chown -R ${USER_NAME}:${USER_NAME} ${ANDROID_HOME}

# --- Gradle properties (global) ---
RUN mkdir -p /home/${USER_NAME}/.gradle && \
    echo 'org.gradle.daemon=true' >> /home/${USER_NAME}/.gradle/gradle.properties && \
    echo 'org.gradle.parallel=true' >> /home/${USER_NAME}/.gradle/gradle.properties && \
    chown -R ${USER_NAME}:${USER_NAME} /home/${USER_NAME}/.gradle

USER ${USER_NAME}
WORKDIR /home/${USER_NAME}

CMD ["bash"]
