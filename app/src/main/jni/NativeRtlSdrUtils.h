#ifndef SHIPS_NATIVERTLSDRUTILS_H
#define SHIPS_NATIVERTLSDRUTILS_H

#ifdef __cplusplus
extern "C" {
#endif

void allocate_args_from_string(const char *string, int nargslength, int *argc, char ***argv);

#ifdef __cplusplus
}
#endif
#endif