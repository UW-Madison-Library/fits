# This file creates a FITS image.
#
# Usage:
#   # Build the image
#   mvn -DskipTests clean package
#   podman build -f docker/Dockerfile -t fits .
#
#   # Run FITS on a file
#   podman run -v `pwd`:/work:z fits -i file.txt
#
#   # Run FITS on a directory
#   podman run -v `pwd`:/work:z fits -r -i in-dir -o out-dir
#
#   # Run FITS with alternate configuration
#   podman run -v `pwd`:/work:z fits -f fits-custom.xml -i file.txt

FROM docker.io/eclipse-temurin:17-jre-focal

ARG FILE_VERSION=5.41

RUN apt-get update

# Install exiftool dependencies https://github.com/exiftool/exiftool
RUN apt-get install -yqq \
    libarchive-zip-perl \
    libio-compress-perl \
    libcompress-raw-zlib-perl \
    libcompress-bzip2-perl \
    libcompress-raw-bzip2-perl \
    libio-digest-perl \
    libdigest-md5-file-perl \
    libdigest-perl-md5-perl \
    libdigest-sha-perl \
    libposix-strptime-perl \
    libunicode-linebreak-perl

# Install file dependencies
RUN apt-get install -yqq \
    make \
    gcc

# Install file https://github.com/file/file
RUN cd /var/tmp && \
    curl -so file-${FILE_VERSION}.tar.gz https://astron.com/pub/file/file-${FILE_VERSION}.tar.gz && \
    tar xzf file-${FILE_VERSION}.tar.gz && \
    cd file-${FILE_VERSION} && \
    ./configure && \
    make -j4 && \
    make install && \
    ldconfig && \
    cd .. && \
    rm -rf file-${FILE_VERSION}*

# Install FITS
RUN apt-get install -yqq unzip
COPY target/fits-*.zip /opt/fits.zip
RUN unzip -q /opt/fits.zip -d /opt/fits && \
    rm /opt/fits.zip && \
    mkdir -p /var/log/fits

# Cleanup package list
RUN  rm -rf /var/lib/apt/lists/*

# Bind the current working directory here so that FITS can access files on the host
WORKDIR /work

ENTRYPOINT ["/opt/fits/fits.sh"]
