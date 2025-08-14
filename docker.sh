docker run -d \
-p 9999:9999 \
-v /data/fileServer:/fileServer \
--restart=unless-stopped \
--name file_server \
file-server