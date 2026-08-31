FROM debian:bookworm-slim

RUN apt-get update \
 && apt-get install -y --no-install-recommends apache2 curl build-essential wget ca-certificates \
 && rm -rf /var/lib/apt/lists/*

# Build the ORIGINAL vulnerable bash 4.3 (pre-CVE-2014-6271 / CVE-2014-7169 patches) from GNU source.
# Only this binary is used to run the CGI script -- Apache itself is a normal current build.
RUN wget -q https://ftp.gnu.org/gnu/bash/bash-4.3.tar.gz \
 && tar xzf bash-4.3.tar.gz \
 && cd bash-4.3 && ./configure --prefix=/opt/vulnbash >/dev/null && make -j2 >/dev/null && make install >/dev/null \
 && cd / && rm -rf bash-4.3 bash-4.3.tar.gz \
 && apt-get purge -y build-essential wget && apt-get autoremove -y && rm -rf /var/lib/apt/lists/*

RUN a2enmod cgi
RUN mkdir -p /usr/lib/cgi-bin && printf '#!/opt/vulnbash/bin/bash\necho "Content-Type: text/plain"\necho ""\necho "status page ok"\ndate\n' > /usr/lib/cgi-bin/status.cgi \
 && chmod +x /usr/lib/cgi-bin/status.cgi
RUN printf '<Directory "/usr/lib/cgi-bin">\n  AllowOverride None\n  Options +ExecCGI -MultiViews +SymLinksIfOwnerMatch\n  Require all granted\n</Directory>\n' > /etc/apache2/conf-available/cgi-enable.conf \
 && a2enconf cgi-enable

EXPOSE 80
CMD ["apache2ctl", "-D", "FOREGROUND"]
