const { query } = require("./db");

async function requireAdultProfile(userId){
  const r=await query(
    `SELECT date_of_birth,
            EXTRACT(YEAR FROM age(CURRENT_DATE,date_of_birth))::int AS age
       FROM profiles
      WHERE user_id=$1
      LIMIT 1`,
    [userId]
  );
  const row=r.rows[0];
  if(!row||!row.date_of_birth){
    const e=new Error("DATE_OF_BIRTH_REQUIRED");e.statusCode=409;throw e;
  }
  const age=Number(row.age||0);
  if(age<18){
    const e=new Error("AGE_18_PLUS_REQUIRED");e.statusCode=403;throw e;
  }
  return {age,dateOfBirth:row.date_of_birth};
}

module.exports={requireAdultProfile};
