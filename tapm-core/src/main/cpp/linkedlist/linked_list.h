//
// Created by pengcheng.tan on 2025/3/25.
//

#ifndef TAPM_LINKED_LIST_H
#define TAPM_LINKED_LIST_H

typedef struct Node {
    Node * next = nullptr;
    Node * previous = nullptr;
    void * value = nullptr;
} Node;

static Node rootNode;

static Node *rootNodePtr = &rootNode;

typedef struct LinkedList {
    Node * head = rootNodePtr;
    Node * tail = rootNodePtr;
    int size = 0;

    void addToFirst(void *v);

    void addToLast(void *v);

    void* popFirst();

    void* popLast();

    void forEach(void *context, bool (*action)(void *value, void *context)) const;

} LinkedList;

#endif //TAPM_LINKED_LIST_H
