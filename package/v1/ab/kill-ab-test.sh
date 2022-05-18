#!/bin/bash

ps aux | grep 'ab -' | grep -v grep | awk '{print $2;}' | xargs kill -9 > /dev/null 2>&1
