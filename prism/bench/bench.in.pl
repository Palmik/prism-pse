{ prism => '../bin/prism'
, models =>
  [ { path => 'models/signalling_2_v6_noreg.sm'
    , opts =>
      [ { parameters =>
          [ { k1 => [0.1, 0.5], k2 => [0.01, 0.015] }
          ]
        , environment =>
          [ {PSE_OCL=>0,PSE_ADAPTIVE_FOX_GLYNN=>1}
          , {PSE_OCL=>0,PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_PARA=>2}
          , {PSE_OCL=>0,PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_PARA=>4}
          , {PSE_OCL=>0,PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_PARA=>8}
          , {PSE_OCL=>1,PSE_ADAPTIVE_FOX_GLYNN=>1}
          , {PSE_OCL=>1,PSE_MANY=>2}
          , {PSE_OCL=>1,PSE_MANY=>4}
          , {PSE_OCL=>1,PSE_MANY=>8}
          ]
        , properties =>
          [ { synth => 1, type => 'thr', csl => 'P<0.025 [ F[100,100] (popR>=10 & popR<=15) ]', acc => 0.1 }
          ]
        }
      ]
    }
  , { path => 'models/AM_39.sm'
    , opts =>
      [ { parameters =>
          [ { k_r_id3 => [1,2], k_r_id4 => [80,90], k_r_id5 => [0.01,0.04] }
          #[ { k_r_id3 => [1,1.0001], k_r_id4 => [80,80.00001], k_r_id5 => [0.01,0.010001] }
          ]
        , properties =>
          [ { synth => 1, type => 'thr', csl => 'P<0.95 [F[100,100] (s_id0=14) & (s_id1=0)]', acc => 0.1 }
          #[ { synth => 1, type => 'thr', csl => 'P>=0.2 [F[10,10] (s_id0=14) & (s_id1=0)]', acc => 0.025 }
          ]
        , environment =>
          [ {PSE_OCL=>0,PSE_ADAPTIVE_FOX_GLYNN=>1}
          , {PSE_OCL=>0,PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_PARA=>4}
          , {PSE_OCL=>0,PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_PARA=>8}
          , {PSE_OCL=>1,PSE_ADAPTIVE_FOX_GLYNN=>1}
          , {PSE_OCL=>1,PSE_MANY=>2}
          , {PSE_OCL=>1,PSE_MANY=>4}
          , {PSE_OCL=>1,PSE_MANY=>8}
          , {PSE_OCL=>1,PSE_MANY=>16}
          , {PSE_OCL=>1,PSE_MANY=>32}
          , {PSE_OCL=>1,PSE_MANY=>64}
          , {PSE_OCL=>1,PSE_MANY=>128}
          , {PSE_OCL=>1,PSE_MANY=>256}
          ]
        }
      ]
    }
  , { path => 'models/sir.sm'
    , opts =>
      [ { parameters =>
          [ { ki => [0.00005, 0.003], kr => [0.005, 0.2] }
          #[ { kr => [0.00005, 0.000050001], ki => [0.005, 0.00500001] }
          ]
        , environment =>
          [ {PSE_OCL=>0,PSE_ADAPTIVE_FOX_GLYNN=>1}
          , {PSE_OCL=>0,PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_PARA=>4}
          , {PSE_OCL=>1,PSE_ADAPTIVE_FOX_GLYNN=>1}
          , {PSE_OCL=>1,PSE_MANY=>2}
          , {PSE_OCL=>1,PSE_MANY=>4}
          , {PSE_OCL=>1,PSE_MANY=>8}
          , {PSE_OCL=>1,PSE_MANY=>16}
          ]
        , properties =>
          [ { synth => 1, type => 'thr', csl => 'P>=0.1 [ (popI>0) U[165,215] (popI=0) ]', acc => 0.1 }
          #[ { synth => 1, type => 'thr', csl => 'P>=0.1 [ (popI>0) U[11,14] (popI=0) ]', acc => 0.1 }
          ]
        }
      ]
    }
  , { path => 'models/g1s-hill.sm'
    , opts =>
      [ { parameters =>
          [ { degrA => [0.005, 0.5], degrB => [0.05, 0.15] }
          #[ { degrA => [0.005, 0.0050001], degrB => [0.05, 0.050001] }
          ]
        , environment =>
          [ {PSE_OCL=>0,PSE_ADAPTIVE_FOX_GLYNN=>1}
          , {PSE_OCL=>0,PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_PARA=>4}
          , {PSE_OCL=>0,PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_PARA=>8}
          , {PSE_OCL=>1,PSE_ADAPTIVE_FOX_GLYNN=>1}
          , {PSE_OCL=>1,PSE_MANY=>2}
          , {PSE_OCL=>1,PSE_MANY=>4}
          , {PSE_OCL=>1,PSE_MANY=>8}
          , {PSE_OCL=>1,PSE_MANY=>16}
          , {PSE_OCL=>1,PSE_MANY=>32}
          , {PSE_OCL=>1,PSE_MANY=>64}
          , {PSE_OCL=>1,PSE_MANY=>128}
          ]
        , properties =>
          [ { synth => 1, type => 'thr', csl => 'P>=0.4 [ F[1000,1000] (B<2) ]', acc => 0.01 }
          #[ { synth => 1, type => 'thr', csl => 'P>=0.4 [ F[10,10] (B<2) ]', acc => 0.1 }
          ]
        }
      ]
    }
  ]
}
