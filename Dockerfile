# Start with a base image that includes OpenJDK 8 on Debian
FROM eclipse-temurin:8u412-b08-jdk-jammy

# Install the required packages in one run to keep the image size down
RUN apt-get update && apt-get install -y \
    netcat-openbsd \
    net-tools \
    curl \
    gnupg \
    libsnappy1v5 \
    libsnappy-dev && \
    # Remove apt cache to minimize the size
    rm -rf /var/lib/apt/lists/* && \
    # Optional: Create a user and group to run your application (instead of running as root)
    addgroup --system appgroup && adduser --system --ingroup appgroup appuser

# Switch to the user just created
USER appuser

# Set the working directory
WORKDIR /home/appuser