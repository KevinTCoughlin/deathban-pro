FROM mcr.microsoft.com/devcontainers/java:25-noble

# Install Gradle
ARG GRADLE_VERSION=9.3.1
RUN curl -fsSL https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip -o /tmp/gradle.zip \
    && unzip -d /opt /tmp/gradle.zip \
    && rm /tmp/gradle.zip \
    && ln -s /opt/gradle-${GRADLE_VERSION}/bin/gradle /usr/local/bin/gradle

# Install useful tools
RUN apt-get update && apt-get install -y --no-install-recommends \
    screen \
    && rm -rf /var/lib/apt/lists/*

# Set Gradle home
ENV GRADLE_HOME=/opt/gradle-${GRADLE_VERSION}
ENV PATH="${GRADLE_HOME}/bin:${PATH}"
