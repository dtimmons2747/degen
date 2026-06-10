import mysql.connector

try:
    conn = mysql.connector.connect(
        host='g7622u.h.filess.io',
        user='degen_db_nobodymain',
        password='737c038c45e2ca0e2f699d2a7612d313efeb9d68',
        database='degen_db_nobodymain'
    )
    
    cursor = conn.cursor()
    
    # Delete the older duplicate entry (1449)
    delete_query = "DELETE FROM player_scorecard WHERE player_scorecard_id = 1449"
    cursor.execute(delete_query)
    conn.commit()
    
    print(f"Deleted scorecard ID 1449")
    print(f"Rows affected: {cursor.rowcount}")
    
    # Verify the deletion
    verify_query = """
    SELECT ps.player_scorecard_id, h.hole_number, ps.gross_score
    FROM player_scorecard ps
    JOIN hole h ON ps.hole_id = h.hole_id
    WHERE ps.player_scorecard_id IN (1449, 1450)
    """
    
    cursor.execute(verify_query)
    results = cursor.fetchall()
    
    if results:
        print("\nRemaining entries:")
        for scorecard_id, hole_number, gross in results:
            print(f"  Scorecard {scorecard_id}: Hole {hole_number} = {gross}")
    else:
        print("\nNo entries found - deletion successful!")
    
    cursor.close()
    conn.close()
    
except Exception as e:
    print(f"Error: {e}")





