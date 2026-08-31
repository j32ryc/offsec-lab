FROM debian:stretch-slim

RUN echo "deb [trusted=yes] http://snapshot.debian.org/archive/debian/20200731T211026Z/ stretch main" > /etc/apt/sources.list \
 && (apt-get -o Acquire::Check-Valid-Until=false update || true) \
 && apt-get -o Acquire::Check-Valid-Until=false install -y --no-install-recommends build-essential curl ca-certificates \
 && rm -rf /var/lib/apt/lists/*

# Build the exact vulnerable OpenSSL release (1.0.1f) that introduced Heartbleed (CVE-2014-0160)
RUN curl -fL -o openssl-1.0.1f.tar.gz https://www.openssl.org/source/old/1.0.1/openssl-1.0.1f.tar.gz \
 && tar xzf openssl-1.0.1f.tar.gz \
 && cd openssl-1.0.1f \
 && ./config --prefix=/opt/vulnssl --openssldir=/opt/vulnssl shared \
 && make depend \
 && make \
 && make install_sw \
 && cd / && rm -rf openssl-1.0.1f openssl-1.0.1f.tar.gz

ENV LD_LIBRARY_PATH=/opt/vulnssl/lib
RUN /opt/vulnssl/bin/openssl version

RUN mkdir -p /certs && /opt/vulnssl/bin/openssl req -x509 -newkey rsa:2048 -nodes \
    -keyout /certs/key.pem -out /certs/cert.pem -days 3650 \
    -subj "/CN=vulnerable-heartbleed-lab.local"

EXPOSE 4433
CMD ["/opt/vulnssl/bin/openssl", "s_server", "-accept", "4433", "-cert", "/certs/cert.pem", "-key", "/certs/key.pem", "-www"]
