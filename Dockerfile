# Use an official OpenJDK image as the base
FROM openjdk:17-jdk-slim

# Install required packages (wget, unzip, etc.)
RUN apt-get update && apt-get install -y wget unzip

# Set environment variables for Android SDK
ENV ANDROID_SDK_ROOT=/sdk
ENV PATH=$PATH:/sdk/cmdline-tools/latest/bin:/sdk/platform-tools

# Download and install Android SDK Command-line Tools
RUN mkdir -p /sdk/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O /cmdline-tools.zip && \
    unzip /cmdline-tools.zip -d /sdk/cmdline-tools && \
    mv /sdk/cmdline-tools/cmdline-tools /sdk/cmdline-tools/latest && \
    rm /cmdline-tools.zip

# Accept licenses and install build tools and platforms
RUN yes | sdkmanager --licenses
RUN sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# Copy your project into the container
WORKDIR /app
COPY . .

# Make gradlew executable
RUN chmod +x ./gradlew

# Build the project (APK)
CMD [\"./gradlew\", \"assembleDebug\"]
