_hale_join() { local d=$1; shift; echo -n "$1"; shift; printf "%s" "${@/#/$d}"; }

_hale()
{
  compopt +o default

  local cur="${COMP_WORDS[COMP_CWORD]}"
  
  # delegate to hale command
  #local joined=$(_hale_join "' '" "${COMP_WORDS[@]}")
  #local joined="'$joined'"
  #local hale_result=$(hale --complete $COMP_CWORD $joined)
  local hale_result=$(hale --complete $COMP_CWORD "${COMP_WORDS[@]}")

  case "${hale_result}" in
    # Reserved word FILE
    FILE)
      compopt -o default; COMPREPLY=()
      return 0
      ;;
    # In all other cases - interpret result as command
    *)
      COMPREPLY=( $(eval $hale_result) )
      return 0
      ;;
  esac

  #TODO support also simple case of value list returned by hale?
  # COMPREPLY=( $(compgen -W "${values}" -- ${cur}) )
}

complete -o default -F _hale hale

