#! /usr/bin/env zsh

_cbt() {
  reply=( "${(ps:\n:)$(cbt taskNames)}" )
}

compctl -K _cbt cbt
