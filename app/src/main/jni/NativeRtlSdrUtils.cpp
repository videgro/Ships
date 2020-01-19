#include <string.h>
#include <stdlib.h>

#ifdef __cplusplus
extern "C" {
#endif

void strcpytrimmed(char *dest, const char *src, int dest_malloced_size) {
    const int charstocopy = dest_malloced_size - 1;

    dest[charstocopy] = 0;

    int firstspaceends;
    for (firstspaceends = 0;
         (firstspaceends < charstocopy) && (src[firstspaceends] == ' '); firstspaceends++);

    int lastspacestarts;
    for (lastspacestarts = charstocopy - 1;
         (lastspacestarts >= firstspaceends) && (src[lastspacestarts] == ' '); lastspacestarts--);

    const int srcrealsize = lastspacestarts - firstspaceends + 1;

    memcpy(dest, &src[firstspaceends], (srcrealsize) * sizeof(char));
}

void allocate_args_from_string(const char *string, int nargslength, int *argc, char ***argv) {
    int i;

    (*argc) = 1;
    for (i = 0; i < nargslength; i++) {
        if (string[i] == ' ') {
            (*argc)++;
        }
    }

    if ((*argc) == nargslength + 1) {
        (*argc) = 0;
        return;
    }

    //(*argv) = malloc(((*argc) + 2) * sizeof(char *));
    (*argv) = (char**)malloc(((*argc) + 2) * sizeof(char *));
    (*argv)[0] = 0;
    int id = 1;
    const char *laststart = string;
    int lastlength = 0;
    for (i = 0; i < nargslength - 1; i++) {
        lastlength++;
        if (string[i] == ' ' && string[i + 1] != ' ') {
            (*argv)[id] = (char *) malloc(lastlength);
            strcpytrimmed((*argv)[id++], laststart, lastlength);

            laststart = &string[i + 1];
            lastlength = 0;
        }
    }
    lastlength++;
    (*argv)[id] = (char *) malloc(lastlength + 1);
    strcpytrimmed((*argv)[id++], laststart, lastlength + 1);
    (*argv)[id] = 0;
    (*argc) = id;
}

#ifdef __cplusplus
}
#endif