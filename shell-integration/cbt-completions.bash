# this does currently not support for completion of nested methods, e.g. snapshot.comp<tab>
# bash 4 should be able to do that via $READLINE_LINE and $READLINE_POINT, see
# http://unix.stackexchange.com/questions/106761/simulating-readline-line-in-bash-4-0/106832
__cbt()
{
  local cur prev opts
  COMPREPLY=()
  cur="${COMP_WORDS[COMP_CWORD]}"
  prev="${COMP_WORDS[COMP_CWORD-1]}"
  opts="$(cbt complete)"
  COMPREPLY=( $(compgen -W "${opts}" -- ${cur}) )
  return 0
}

complete -F __cbt cbt
