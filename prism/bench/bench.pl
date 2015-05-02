use strict;
use warnings;

use File::Spec qw();
use Digest::SHA qw();

sub main
{
  if (scalar @ARGV < 2) {
    print "Usage: $0 <path to bench definition> <number of repetitions>\n";
    return;
  }
  my $def = do $ARGV[0];
  if (my $err = $@) {
    die $@;
  }
  my $rep = $ARGV[1];
  my $output_dir = $ARGV[2];

  for my $model (@{$def->{models}}) {
    for my $opts (@{$model->{opts}}) {
      $opts->{environment} //= [{}];
      for my $parameters (@{$opts->{parameters}}) {
        for my $env (@{$opts->{environment}}) {
          for my $property (@{$opts->{properties}}) {
            _handle($env, $def->{prism}, $model->{path}, $parameters, $property, $rep, $output_dir);
          }
        }
      }
    }
  }
}

sub _handle
{
  my ($env, $prism, $model, $parameters, $property, $rep, $output_dir) = @_;
  my $command = undef;

  $env->{PRISM_JAVAMAXMEM} = '3g';
  $command = _get_pse_command_string($prism, $model, $parameters, $property, $env);
  _run_command($command, $rep, $output_dir) if $command;

  $env->{PRISM_JAVAMAXMEM} = '3g';
  $command = _get_reg_command_string($prism, $model, $parameters, $property, $env);
  _run_command($command, $rep, $output_dir) if $command;
}

sub _run_command
{
  my ($command, $rep, $output_dir) = @_;
  print "$command\n";
  my $avg = 0;
  for (1..$rep) {
    my ($t, $out) = _run_command_once($command);
    if ($_ == 1) {
      _store_command_output($command, $output_dir, $out);
      _print_model_data($out);
    }
    if (defined $t) {
      print "$t\n";
      $avg += $t;
    } else {
      print "could not parse out\n";
    }
  }
  $avg /= $rep;
  print "$avg\n";
}

sub _run_command_once
{
  my ($command, $output_dir) = @_;

  my $out = `$command 2>&1`;
  if ($out =~ qr/Total time for model checking: ([0-9,.]*).*/) {
    return ($1, $out);
  } elsif ($out =~ qr/Time for parameter space exploration: ([0-9,.]*).*/) {
    return ($1, $out);
  } elsif ($out =~ qr/Time for transient probability computation: ([0-9,.]*).*/) {
    return ($1, $out);
  } elsif ($out =~ qr/Time for model checking: ([0-9,.]*).*/) {
    return ($1, $out);
  } else {
    return 0;
  }
}

sub _get_pse_command_string
{
  my ($prism, $model, $parameters, $property, $env) = @_;
  my $env_str = _get_env_string($env);
  my $parameters_str = _get_parameters_string($parameters);

  my $command = "env $env_str $prism $model ";
  if ($property->{transient}) {
    $command .= "-pse $property->{time} $parameters_str $property->{acc}";
  } elsif ($property->{check}) {
    $command .= "-psecheck $parameters_str $property->{acc} -csl '$property->{csl}'";
  } elsif ($property->{synth}) {
    $command .= "-psesynth-$property->{type} $parameters_str $property->{acc} -csl '$property->{csl}'";
  } else {
    return undef;
  }
  return $command;
}

sub _get_reg_command_string
{
  my ($prism, $model, $parameters, $property, $env) = @_;
  my $env_str = _get_env_string($env);
  my $parameters_str = _get_parameters_string($parameters);
  if (not _is_parametrised($parameters)) {
    my $command = "env $env_str $prism $model ";
    if ($property->{transient}) {
      $command .= "-sparse -nossdetect -const $parameters_str -tr $property->{time}";
    } elsif ($property->{check}) {
      $command .= "-sparse -nossdetect -const $parameters_str -csl '$property->{csl}'";
    } else {
      return undef;
    }
    return $command;
  } else {
    return undef;
  }
}

sub _get_env_string
{
  my ($env) = @_;
  my $env_str = '';
  for my $k (sort keys %$env) {
    $env_str .= " " if $env_str;
    $env_str .= "$k=$env->{$k}";
  }
  return $env_str;
}

sub _get_parameters_string
{
  my ($par) = @_;
  my $par_str = '';
  for my $p (sort keys %$par) {
    $par_str .= "," if $par_str;
    $par_str .= "$p=$par->{$p}[0]:$par->{$p}[1]";
  }
  return $par_str;
}

sub _is_parametrised
{
  my ($parameters) = @_;
  for my $p (keys %$parameters) {
    if ($parameters->{$p}[0] ne $parameters->{$p}[1]) {
      return 1;
    }
  }
  return 0;
}

sub _store_command_output
{
  my ($command, $output_dir, $out) = @_;
  return unless defined $output_dir;
  my $output_path = File::Spec->catfile($output_dir, _get_output_file_name($command));
  print "$output_path\n";
  open(my $fh, '>', $output_path) or print "could not open out file, reason: $!\n";
  print $fh $out if $fh;
}

sub _get_output_file_name
{
  my ($command) = @_;
  return Digest::SHA::sha1_hex($command) . ".out";
}

sub _print_model_data
{
  my ($out) = @_;
  my %res = ();
  $res{state_cnt} = $1 if ($out =~ qr/state[^\d]*(\d+)/i);
  $res{trans_cnt} = $1 if ($out =~ qr/trans[^\d]*(\d+)/i);
  my $res_str = '';
  for my $k (sort keys %res) {
    $res_str .= "," if $res_str;
    $res_str .= "$k=$res{$k}";
  }
  print "$res_str\n" if $res_str;
}

main();
