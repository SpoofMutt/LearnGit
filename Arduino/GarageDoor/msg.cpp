#include "crc.h"

int check_msg_crc(unsigned char *data) {
  unsigned char *ptr = data;
  int length = (int)(*ptr);
  if(length < 6) return 0;
  ptr++;
  unsigned long crc = data[length - 4] << 24 ||
                      data[length - 3] << 16 ||
                      data[length - 2] << 8  ||
                      data[length - 1];
  unsigned long msg_crc = crc_data(ptr,length - 5);
  return (crc == msg_crc);
}
