import React, { useState } from "react";

const InquiryCommentInput = () => {
  const [value, setValue] = useState("");
  const onKeyUp = (event) => {
    setValue(event.target.value);
  };

  const onSubmit = (event) => {
    event.preventDefault();
    // TODO:value 제출
    setValue("");
  };

  return (
    <div>
      <form onSubmit={onSubmit}>
        <input onKeyUp={onKeyUp} />
        <button type="submit">제출</button>
      </form>
      <hr />
    </div>
  );
};

export default InquiryCommentInput;
