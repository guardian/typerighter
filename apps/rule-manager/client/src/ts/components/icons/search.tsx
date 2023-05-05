interface SVGRProps {
  title?: string;
  titleId?: string;
}

export const icon = ({
  title,
  titleId,
  ...props
}: React.SVGProps<SVGSVGElement> & SVGRProps) =>
  <svg
    xmlns="http://www.w3.org/2000/svg"
    width={24}
    height={24}
    aria-labelledby={titleId}
    {...props}
    viewBox="0 0 23 23">
    <path fillRule="evenodd" clipRule="evenodd"
          d="M9.273 2c4.023 0 7.25 3.295 7.25 7.273a7.226 7.226 0 0 1-7.25 7.25C5.25 16.523 2 13.296 2 9.273 2 5.295 5.25 2 9.273 2Zm0 1.84A5.403 5.403 0 0 0 3.84 9.274c0 3 2.409 5.454 5.432 5.454 3 0 5.454-2.454 5.454-5.454 0-3.023-2.454-5.432-5.454-5.432Zm7.295 10.887L22 20.16 20.16 22l-5.433-5.432v-.932l.91-.909h.931Z"></path>
  </svg>
