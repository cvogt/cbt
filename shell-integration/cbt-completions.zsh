# this does currently not support for completion of nested methods, e.g. snapshot.comp<tab>
# zsh should be able to do that via $BUFFER, see
# http://unix.stackexchange.com/questions/14230/zsh-tab-completion-on-empty-line

_cbt() {
  reply=( "${(ps:\n:)$(cbt complete)}" )
}

compctl -K _cbt cbt
