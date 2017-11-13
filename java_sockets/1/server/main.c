
/* for be64toh */
#define _BSD_SOURCE

#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <inttypes.h>

/* for be64toh */
#include <endian.h>

#define BUFSIZE 9
#define REQUEST_QUEUE 5

void error(char *msg) {
    perror(msg);
    exit(1);
}

int8_t decode(void *buff, ssize_t read, int64_t *out) {
    /* encoding: [type|data] where type is number of bytes */
    int8_t type_length = ((int8_t*) buff)[0];
    void *data = ((uint8_t*) buff) + 1;
    switch(type_length) {
        case 1:
            *out = *((int8_t *) data);
            break;
        case 2:
            *out = ntohs(*((int16_t *) data));
            break;
        case 4:
            *out = ntohl(*((int32_t *) data));
            break;
        case 8:
            *out = be64toh(*((int64_t *) data));
            break;
        default:
            fprintf(stderr, "Unknown type received!");
            return -1;
    }

    return 0;
}

int main(int argc, char **argv) {
    int portno;
    struct sockaddr_in serveraddr;
    char buf[BUFSIZE];

    if (argc < 2) {
        fprintf(stderr, "usage: %s <port>\n", argv[0]);
        exit(1);
    }
    portno = atoi(argv[1]);

    int listening_sock = socket(AF_INET, SOCK_STREAM, 0);
    if (listening_sock < 0)
        error("ERROR opening socket");

    /* Handy debugging trick that lets
     * us rerun the server immediately after we kill it;
     * otherwise we have to wait about 20 secs.
     * Eliminates "ERROR on binding: Address already in use" error.
     */
    int optval = 1;
    setsockopt(listening_sock, SOL_SOCKET, SO_REUSEADDR,
               (const void *)&optval , sizeof(int));

    /*
     * build the server's Internet address
     */
    memset(&serveraddr, 0, sizeof(serveraddr));

    serveraddr.sin_family = AF_INET;
    serveraddr.sin_addr.s_addr = htonl(INADDR_ANY);
    serveraddr.sin_port = htons((unsigned short)portno);

    /*
     * bind: associate the parent socket with a port
     */
    if (bind(listening_sock, (struct sockaddr *) &serveraddr,
             sizeof(serveraddr)) < 0)
        error("ERROR on binding");

    /*
     * listen: make this socket ready to accept connection requests
     */
    if (listen(listening_sock, REQUEST_QUEUE) < 0) /* allow 5 requests to queue up */
        error("ERROR on listen");

    /*
     * main loop
     */
    int client_sock;
    struct sockaddr_in clientaddr;
    int n;
    int clientlen = sizeof(clientaddr);
    int8_t decode_outcome = 0;
    int64_t received = 0;
    int8_t to_client = 0;
    while (1) {

        /*
         * accept: wait for a connection request
         */
        client_sock = accept(listening_sock, (struct sockaddr *) &clientaddr, &clientlen);

        if (client_sock < 0) {
            perror("ERROR on accept");
        } else {
            memset(buf, 0, BUFSIZE);
            n = read(client_sock, buf, BUFSIZE);
            if (n < 0) {
                perror("ERROR reading from socket");
            } else {
                received = 0;
                decode_outcome = decode(buf, n, &received);
                if(decode_outcome != 0) {
                    fprintf(stderr, "ERROR while decoding");
                } else {
                    to_client = (int8_t) (received % 128);
                    printf("Received: %" PRId64 " Sending back: %d\n", received, (int) to_client);
                    fflush(stdout);

                    n = write(client_sock, &to_client, 1);
                    if (n < 0)
                        perror("ERROR writing to socket");
                }
            }

            close(client_sock);
        }
    }
}