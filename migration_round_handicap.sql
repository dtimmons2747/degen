-- Migration script to populate round_handicap table from tournament_handicap
-- This script initializes round handicaps based on existing tournament handicaps for all tee times

-- Step 2: Insert tournament handicaps for player 1
INSERT INTO round_handicap (round_tee_time_id, player_id, handicap)
SELECT 
    rtt.round_tee_time_id,
    rtt.player1_id,
    th.handicap
FROM round_tee_time rtt
INNER JOIN tournament_round tr ON rtt.tournament_round_id = tr.tournament_round_id
INNER JOIN tournament_handicap th ON tr.tournament_id = th.tournament_id AND rtt.player1_id = th.player_id
WHERE rtt.player1_id IS NOT NULL
    AND th.handicap IS NOT NULL
    AND NOT EXISTS (
        SELECT 1 FROM round_handicap rh 
        WHERE rh.round_tee_time_id = rtt.round_tee_time_id 
        AND rh.player_id = rtt.player1_id
    )
ON DUPLICATE KEY UPDATE handicap = VALUES(handicap);

-- Step 3: Insert tournament handicaps for player 2
INSERT INTO round_handicap (round_tee_time_id, player_id, handicap)
SELECT 
    rtt.round_tee_time_id,
    rtt.player2_id,
    th.handicap
FROM round_tee_time rtt
INNER JOIN tournament_round tr ON rtt.tournament_round_id = tr.tournament_round_id
INNER JOIN tournament_handicap th ON tr.tournament_id = th.tournament_id AND rtt.player2_id = th.player_id
WHERE rtt.player2_id IS NOT NULL
    AND th.handicap IS NOT NULL
    AND NOT EXISTS (
        SELECT 1 FROM round_handicap rh 
        WHERE rh.round_tee_time_id = rtt.round_tee_time_id 
        AND rh.player_id = rtt.player2_id
    )
ON DUPLICATE KEY UPDATE handicap = VALUES(handicap);

-- Step 4: Insert tournament handicaps for player 3
INSERT INTO round_handicap (round_tee_time_id, player_id, handicap)
SELECT 
    rtt.round_tee_time_id,
    rtt.player3_id,
    th.handicap
FROM round_tee_time rtt
INNER JOIN tournament_round tr ON rtt.tournament_round_id = tr.tournament_round_id
INNER JOIN tournament_handicap th ON tr.tournament_id = th.tournament_id AND rtt.player3_id = th.player_id
WHERE rtt.player3_id IS NOT NULL
    AND th.handicap IS NOT NULL
    AND NOT EXISTS (
        SELECT 1 FROM round_handicap rh 
        WHERE rh.round_tee_time_id = rtt.round_tee_time_id 
        AND rh.player_id = rtt.player3_id
    )
ON DUPLICATE KEY UPDATE handicap = VALUES(handicap);

-- Step 5: Insert tournament handicaps for player 4
INSERT INTO round_handicap (round_tee_time_id, player_id, handicap)
SELECT 
    rtt.round_tee_time_id,
    rtt.player4_id,
    th.handicap
FROM round_tee_time rtt
INNER JOIN tournament_round tr ON rtt.tournament_round_id = tr.tournament_round_id
INNER JOIN tournament_handicap th ON tr.tournament_id = th.tournament_id AND rtt.player4_id = th.player_id
WHERE rtt.player4_id IS NOT NULL
    AND th.handicap IS NOT NULL
    AND NOT EXISTS (
        SELECT 1 FROM round_handicap rh 
        WHERE rh.round_tee_time_id = rtt.round_tee_time_id 
        AND rh.player_id = rtt.player4_id
    )
ON DUPLICATE KEY UPDATE handicap = VALUES(handicap);

-- Step 6: Verify the migration
-- Run this to see how many records were migrated:
-- SELECT COUNT(*) as total_round_handicaps FROM round_handicap;
-- SELECT 
--     rtt.round_tee_time_id,
--     rtt.player1_id, rtt.player2_id, rtt.player3_id, rtt.player4_id,
--     COUNT(rh.round_handicap_id) as handicap_count
-- FROM round_tee_time rtt
-- LEFT JOIN round_handicap rh ON rtt.round_tee_time_id = rh.round_tee_time_id
-- GROUP BY rtt.round_tee_time_id
-- ORDER BY rtt.round_tee_time_id;

-- Note: The old columns (player1_handicap, player2_handicap, player3_handicap, player4_handicap)
-- should be dropped AFTER verifying the migration was successful and the backend is working correctly.
-- Uncomment the lines below only after full validation:
-- ALTER TABLE round_tee_time DROP COLUMN player1_handicap;
-- ALTER TABLE round_tee_time DROP COLUMN player2_handicap;
-- ALTER TABLE round_tee_time DROP COLUMN player3_handicap;
-- ALTER TABLE round_tee_time DROP COLUMN player4_handicap;
