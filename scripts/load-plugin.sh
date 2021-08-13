#!/bin/bash

mkdir -p /godata/plugins/external
cp /plugins/*  /godata/plugins/external
chown -R 1000 /godata/plugins/external
