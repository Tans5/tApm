//
// Created by pengcheng.tan on 2025/3/25.
//
#include "linked_list.h"

void LinkedList::addToFirst(void *v) {
    auto node = new Node;
    node->value = v;
    if (size == 0) {
        node->previous = rootNodePtr;
        node->next = rootNodePtr;
        head = node;
        tail = node;
    } else {
        auto oldHead = head;
        oldHead->previous = node;
        node->next = oldHead;
        node->previous = rootNodePtr;
        head = node;
    }
    size ++;
}

void LinkedList::addToLast(void *v) {
    auto node = new Node;
    node->value = v;
    if (size == 0) {
        node->previous = rootNodePtr;
        node->next = rootNodePtr;
        head = node;
        tail = node;
    } else {
        auto oldTail = tail;
        oldTail->next = node;
        node->next = rootNodePtr;
        node->previous = oldTail;
        tail = node;
    }
    size ++;
}

void* LinkedList::popFirst() {
    if (size == 0) return nullptr;
    if (size == 1) {
        auto n = head;
        auto value = n->value;
        delete n;
        head = rootNodePtr;
        tail = rootNodePtr;
        size --;
        return value;
    } else {
        auto oldHead = head;
        auto newHead = head->next;
        newHead->previous = rootNodePtr;
        head = newHead;
        auto value = oldHead->value;
        delete oldHead;
        size --;
        return value;
    }
}

void* LinkedList::popLast() {
    if (size == 0) return nullptr;
    if (size == 1) {
        auto n = head;
        auto value = n->value;
        delete n;
        head = rootNodePtr;
        tail = rootNodePtr;
        size --;
        return value;
    } else {
        auto oldTail = tail;
        auto newTail = tail->previous;
        newTail->next = rootNodePtr;
        tail = newTail;
        auto value = oldTail->value;
        delete oldTail;
        size --;
        return value;
    }
}
